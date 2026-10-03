import { useEffect, useMemo, useState } from 'react'
import './App.css'
import 'bootstrap/dist/css/bootstrap.min.css'
import ProductList from './ProductList'
import CategoryFilter from './CategoryFilter'
import { fetchCategories, fetchProducts } from './api/productsApi'

const PAGE_SIZE = 6

function App() {
  const [products, setProducts] = useState([])
  const [categories, setCategories] = useState([])
  const [selectedCategory, setSelectedCategory] = useState('')
  const [searchTerm, setSearchTerm] = useState('')
  const [sortOrder, setSortOrder] = useState('price,asc')
  const [currentPage, setCurrentPage] = useState(1)
  const [totalPages, setTotalPages] = useState(1)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    const loadCategories = async () => {
      try {
        const data = await fetchCategories()
        setCategories(data)
      } catch (loadError) {
        console.error(loadError)
      }
    }

    loadCategories()
  }, [])

  useEffect(() => {
    const loadProducts = async () => {
      setLoading(true)
      setError('')

      try {
        const response = await fetchProducts({
          page: currentPage - 1,
          size: PAGE_SIZE,
          search: searchTerm,
          categoryId: selectedCategory ? Number(selectedCategory) : null,
          sort: sortOrder,
        })

        const items = Array.isArray(response) ? response : response.items || []
        const pageMeta = Array.isArray(response) ? { totalPages: 1, page: 0 } : response
        setProducts(items)
        setTotalPages(Math.max(1, Number(pageMeta.totalPages || 1)))
      } catch (loadError) {
        setError(loadError.message || 'Unable to load products.')
        setProducts([])
      } finally {
        setLoading(false)
      }
    }

    loadProducts()
  }, [selectedCategory, searchTerm, sortOrder, currentPage])

  const pageNumbers = useMemo(() => {
    return Array.from({ length: totalPages }, (_, index) => index + 1)
  }, [totalPages])

  const handleSearchChange = (event) => {
    setSearchTerm(event.target.value)
    setCurrentPage(1)
  }

  const handleSortChange = (event) => {
    setSortOrder(event.target.value)
    setCurrentPage(1)
  }

  const handleCategorySelect = (categoryId) => {
    setSelectedCategory(categoryId)
    setCurrentPage(1)
  }

  return (
    <div className='container py-4'>
      <h1 className='mb-4'>Product Catalog</h1>

      <div className='row align-items-center mb-4 g-3'>
        <div className='col-md-3 col-sm-12'>
          <CategoryFilter categories={categories} onSelect={handleCategorySelect} selectedValue={selectedCategory} />
        </div>

        <div className='col-md-5 col-sm-12'>
          <input
            type='text'
            className='form-control'
            placeholder='Search for products'
            value={searchTerm}
            onChange={handleSearchChange}
          />
        </div>

        <div className='col-md-4 col-sm-12'>
          <select className='form-control' value={sortOrder} onChange={handleSortChange}>
            <option value='price,asc'>Sort by Price: Low to High</option>
            <option value='price,desc'>Sort by Price: High to Low</option>
          </select>
        </div>
      </div>

      {error ? (
        <div className='alert alert-danger' role='alert'>
          {error}
        </div>
      ) : null}

      {loading ? (
        <div className='text-center my-4'>Loading products...</div>
      ) : products.length ? (
        <>
          <ProductList products={products} />

          <div className='d-flex justify-content-center mt-4 gap-2 flex-wrap'>
            <button
              type='button'
              className='btn btn-outline-primary'
              onClick={() => setCurrentPage((page) => Math.max(1, page - 1))}
              disabled={currentPage === 1}
            >
              Prev
            </button>

            {pageNumbers.map((pageNumber) => (
              <button
                key={pageNumber}
                type='button'
                className={`btn ${pageNumber === currentPage ? 'btn-primary' : 'btn-outline-secondary'}`}
                onClick={() => setCurrentPage(pageNumber)}
              >
                {pageNumber}
              </button>
            ))}

            <button
              type='button'
              className='btn btn-outline-primary'
              onClick={() => setCurrentPage((page) => Math.min(totalPages, page + 1))}
              disabled={currentPage >= totalPages}
            >
              Next
            </button>
          </div>
        </>
      ) : (
        <p className='text-muted'>No products to display.</p>
      )}
    </div>
  )
}

export default App
